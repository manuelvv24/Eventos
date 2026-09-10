package Eventos.eventos.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String authHeader = request.getHeader("Authorization");

        System.out.println("[JWT-FILTER] Path: " + path + " | Auth: " + (authHeader != null));

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            boolean valido = jwtUtil.esTokenValido(token);
            System.out.println("[JWT-FILTER] Token válido: " + valido);

            if (valido) {
                String email = jwtUtil.extraerEmail(token);
                String rol = jwtUtil.extraerRol(token);
                Long idUsuario = jwtUtil.extraerIdUsuario(token);

                SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + rol);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(email, idUsuario, List.of(authority));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                System.out.println("[JWT-FILTER] Auth seteado para: " + email + " | rol: " + rol);
            }
        }

        filterChain.doFilter(request, response);
    }
}
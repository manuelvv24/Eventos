<template>
  <div class="flex h-screen overflow-hidden bg-background font-sans">
    <!-- Sidebar -->
    <AppSidebar :mobile-open="sidebarOpen" @close="sidebarOpen = false" />

    <!-- Main canvas -->
    <div class="flex flex-col flex-1 min-w-0 h-full overflow-hidden">
      <!-- Top bar -->
      <AppTopBar @toggle-sidebar="sidebarOpen = !sidebarOpen" />

      <!-- Page content -->
      <main class="flex-1 overflow-y-auto custom-scrollbar">
        <RouterView />
      </main>
    </div>

    <!-- Diálogo de confirmación global -->
    <AppConfirmDialog />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import AppSidebar from './AppSidebar.vue'
import AppTopBar  from './AppTopBar.vue'
import AppConfirmDialog from '../ui/AppConfirmDialog.vue'

const sidebarOpen = ref(false)
</script>

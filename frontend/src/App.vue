<script setup lang="ts">
// Root shell: header + routed view.
</script>

<template>
  <div class="app">
    <header class="app__header">
      <h1 class="app__title">AI Story Co-Author</h1>
      <nav class="app__nav">
        <RouterLink to="/">首页</RouterLink>
      </nav>
    </header>
    <main class="app__main">
      <RouterView />
    </main>
  </div>
</template>

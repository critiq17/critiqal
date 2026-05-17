<script lang="ts">
  import { page } from '$app/state';
</script>

<div class="error-page">
  <h1>{page.status}</h1>
  <p>{page.error?.message ?? 'Something went wrong'}</p>
  <a href="/">Go home</a>
</div>

<style>
  .error-page {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100vh;
    gap: 1rem;
    font-family: system-ui, sans-serif;
  }
  h1 {
    font-size: 4rem;
    margin: 0;
  }
  p {
    color: #666;
  }
  a {
    color: #007aff;
    text-decoration: none;
  }
</style>

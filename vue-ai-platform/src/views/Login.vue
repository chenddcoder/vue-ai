<template>
  <div style="display: flex; justify-content: center; align-items: center; height: 100vh;">
    <a-card title="Login" style="width: 300px">
      <a-form>
        <a-form-item label="Username">
          <a-input />
        </a-form-item>
        <a-form-item label="Password">
          <a-input-password />
        </a-form-item>
        <a-button type="primary" block>Login</a-button>
      </a-form>
    </a-card>
  </div>
</template>

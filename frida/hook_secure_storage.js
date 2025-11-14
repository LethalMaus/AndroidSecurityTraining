Java.perform(() => {
  const H = Java.use('dev.jamescullimore.android_security_training.storage.SecureStorageHelper');

  // Pick the exact overload explicitly (good habit)
  const saveTokenOverload = H.saveTokenSecure.overload(
    'android.content.Context',
    'java.lang.String',
    'w1.c'
  );

  saveTokenOverload.implementation = function (ctx, token, extra) {
    console.log('[Frida] saveTokenSecure token =', token);
    console.log('[Frida] saveTokenSecure extra =', extra);

    // Always call original with ALL args
    return saveTokenOverload.call(this, ctx, token, extra);
    // or: return this.saveTokenSecure(ctx, token, extra);
  };
});
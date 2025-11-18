Java.perform(() => {
  const H = Java.use('dev.jamescullimore.android_security_training.storage.SecureStorageHelper');

  const saveTokenOverload = H.saveTokenSecure.overload(
    'android.content.Context',
    'java.lang.String',
    'kotlin.coroutines.Continuation'
  );

  saveTokenOverload.implementation = function (ctx, token, cont) {
    console.log('[Frida] saveTokenSecure token =', token);

    const result = saveTokenOverload.call(this, ctx, token, cont);
    console.log('[Frida] original returned:', result);
    return result;
  };
});

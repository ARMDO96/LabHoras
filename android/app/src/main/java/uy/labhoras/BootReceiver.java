package uy.labhoras;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Tras reiniciar el teléfono o actualizar la app, vuelve a mostrar el reloj si estabas marcado. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        long start = Clock.start(context);
        if (start > 0) Clock.setState(context, start);
    }
}

package uy.labhoras;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Muestra el recordatorio de "¿te olvidaste de marcar la salida?". */
public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Clock.postReminder(context);
    }
}

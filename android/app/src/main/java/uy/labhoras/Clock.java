package uy.labhoras;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.service.quicksettings.TileService;

import java.util.Calendar;
import java.util.Locale;

/**
 * Estado nativo del reloj de marcar: notificación mientras estás adentro,
 * recordatorio si te olvidás de marcar la salida y estado del botón rápido.
 * La fuente de verdad son los datos de la página; esto es un reflejo para Android.
 */
final class Clock {
    static final String ACTION_PUNCH = "uy.labhoras.MARCAR";

    private static final String PREFS = "reloj";
    private static final String KEY_START = "start";
    private static final String CH_ONGOING = "reloj_en_curso";
    private static final String CH_REMINDER = "recordatorio_salida";
    private static final int NOTIF_ONGOING = 1;
    private static final int NOTIF_REMINDER = 2;
    private static final long REMIND_AFTER_MS = 8L * 60 * 60 * 1000;
    private static final int COLOR = 0xFF00A6BF;

    private Clock() {
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static long start(Context c) {
        return prefs(c).getLong(KEY_START, 0L);
    }

    static String hhmm(long t) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(t);
        return String.format(Locale.US, "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
    }

    /** start = hora de entrada en milisegundos, o 0 si no estás marcado. */
    static void setState(Context c, long start) {
        prefs(c).edit().putLong(KEY_START, start).apply();
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        ensureChannels(nm);
        if (start > 0) {
            nm.notify(NOTIF_ONGOING, ongoing(c, start));
            scheduleReminder(c, start);
        } else {
            nm.cancel(NOTIF_ONGOING);
            nm.cancel(NOTIF_REMINDER);
            alarms(c).cancel(reminderIntent(c));
        }
        TileService.requestListeningState(c, new ComponentName(c, ClockTileService.class));
    }

    static PendingIntent openApp(Context c, boolean punch) {
        Intent i = new Intent(c, MainActivity.class);
        i.setAction(punch ? ACTION_PUNCH : Intent.ACTION_MAIN);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(c, punch ? 1 : 0, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static Notification.Action exitAction(Context c) {
        return new Notification.Action.Builder(
                Icon.createWithResource(c, R.drawable.ic_stat_clock),
                c.getString(R.string.punch_out), openApp(c, true)).build();
    }

    private static Notification ongoing(Context c, long start) {
        return new Notification.Builder(c, CH_ONGOING)
                .setSmallIcon(R.drawable.ic_stat_clock)
                .setColor(COLOR)
                .setContentTitle(c.getString(R.string.notif_in_lab))
                .setContentText(c.getString(R.string.notif_since, hhmm(start)))
                .setWhen(start)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_STATUS)
                .setContentIntent(openApp(c, false))
                .addAction(exitAction(c))
                .build();
    }

    /** Lo llama ReminderReceiver cuando pasaron REMIND_AFTER_MS desde la entrada. */
    static void postReminder(Context c) {
        long start = start(c);
        if (start <= 0) return;
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        ensureChannels(nm);
        Notification n = new Notification.Builder(c, CH_REMINDER)
                .setSmallIcon(R.drawable.ic_stat_clock)
                .setColor(COLOR)
                .setContentTitle(c.getString(R.string.reminder_title))
                .setContentText(c.getString(R.string.reminder_text, hhmm(start)))
                .setStyle(new Notification.BigTextStyle().bigText(c.getString(R.string.reminder_text, hhmm(start))))
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_REMINDER)
                .setContentIntent(openApp(c, true))
                .addAction(exitAction(c))
                .build();
        nm.notify(NOTIF_REMINDER, n);
    }

    private static AlarmManager alarms(Context c) {
        return c.getSystemService(AlarmManager.class);
    }

    private static PendingIntent reminderIntent(Context c) {
        return PendingIntent.getBroadcast(c, 0, new Intent(c, ReminderReceiver.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static void scheduleReminder(Context c, long start) {
        long at = start + REMIND_AFTER_MS;
        if (at <= System.currentTimeMillis()) return;
        // Alarma inexacta: no requiere permisos especiales; puede demorar unos minutos.
        alarms(c).set(AlarmManager.RTC_WAKEUP, at, reminderIntent(c));
    }

    private static void ensureChannels(NotificationManager nm) {
        if (nm.getNotificationChannel(CH_ONGOING) == null) {
            NotificationChannel ch = new NotificationChannel(CH_ONGOING, "Reloj en curso", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Muestra desde qué hora estás marcado en el laboratorio.");
            ch.setShowBadge(false);
            nm.createNotificationChannel(ch);
        }
        if (nm.getNotificationChannel(CH_REMINDER) == null) {
            NotificationChannel ch = new NotificationChannel(CH_REMINDER, "Recordatorio de salida", NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Avisa si pasaron muchas horas sin marcar la salida.");
            nm.createNotificationChannel(ch);
        }
    }
}

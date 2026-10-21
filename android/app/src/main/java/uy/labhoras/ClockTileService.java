package uy.labhoras;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/** Botón rápido "Marcar" en el panel de notificaciones: abre la app y pide la huella. */
public class ClockTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        refresh();
    }

    private void refresh() {
        Tile tile = getQsTile();
        if (tile == null) return;
        long start = Clock.start(this);
        tile.setIcon(Icon.createWithResource(this, R.drawable.ic_stat_clock));
        tile.setLabel(getString(R.string.tile_label));
        tile.setState(start > 0 ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setSubtitle(start > 0 ? getString(R.string.tile_in, Clock.hhmm(start)) : getString(R.string.tile_out));
        tile.updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        if (isLocked()) unlockAndRun(this::launch);
        else launch();
    }

    @SuppressWarnings("deprecation")
    private void launch() {
        PendingIntent pi = Clock.openApp(this, true);
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(pi);
        } else {
            Intent i = new Intent(this, MainActivity.class)
                    .setAction(Clock.ACTION_PUNCH)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivityAndCollapse(i);
        }
    }
}

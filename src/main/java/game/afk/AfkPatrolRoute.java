package game.afk;

import game.ai.Position;
import lombok.Getter;

/**
 * Rute patrol kotak searah jarum jam di sekitar spawn.
 *
 * Layout (top-down, Y bertambah ke bawah):
 *   [3] NW ──────► [0] NE
 *    ▲                │
 *    │                ▼
 *   [2] SW ◄────── [1] SE
 *
 * Urutan: NE → SE → SW → NW → (selesai, kembali ke IDLE)
 */
public class AfkPatrolRoute {

    private final Position[] waypoints;

    @Getter private int currentIndex = 0;
    @Getter private int visitCount   = 0;

    public AfkPatrolRoute(Position spawn, int radius) {
        short cx = spawn.getX();
        short cy = spawn.getY();
        short r  = (short) radius;

        waypoints = new Position[]{
            new Position((short)(cx + r), (short)(cy - r)), // [0] NE
            new Position((short)(cx + r), (short)(cy + r)), // [1] SE
            new Position((short)(cx - r), (short)(cy + r)), // [2] SW
            new Position((short)(cx - r), (short)(cy - r)), // [3] NW
        };
    }

    public Position current() {
        return waypoints[currentIndex];
    }

    public int size() {
        return waypoints.length;
    }

    /** Tandai waypoint selesai dan maju ke berikutnya. */
    public void advance() {
        visitCount++;
        currentIndex = (currentIndex + 1) % waypoints.length;
    }

    /** True jika semua 4 sudut telah dikunjungi dalam satu lap. */
    public boolean isComplete() {
        return visitCount >= waypoints.length;
    }

    /** Reset untuk lap baru mulai dari NE. */
    public void reset() {
        currentIndex = 0;
        visitCount   = 0;
    }
}
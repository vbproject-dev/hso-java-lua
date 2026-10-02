package game.event.kemerdekaan;

/**
 * Status alur event Persimpangan Kematian.
 *
 *  CLOSED       → event tidak aktif / belum dimulai
 *  REGISTRATION → pendaftaran player (15 menit)
 *  COUNTDOWN    → jeda sebelum wave pertama (30 detik)
 *  WAVE         → wave monster sedang berjalan
 *  REST         → jeda antar wave (30 detik)
 *  VICTORY      → semua wave selesai, kristal masih hidup
 *  DEFEAT       → kristal hancur
 */
public enum PersimpanganState {
    CLOSED,
    REGISTRATION,
    COUNTDOWN,
    WAVE,
    REST,
    VICTORY,
    DEFEAT
}

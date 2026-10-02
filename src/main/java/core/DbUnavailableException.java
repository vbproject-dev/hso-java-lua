package core;

/**
 * Dilempar saat gagal mengambil koneksi dari pool (pool penuh / timeout /
 * interrupted), BUKAN saat query berhasil jalan tapi hasilnya kosong.
 *
 * Sebelumnya SQL.select() menelan semua SQLException dan return null,
 * sehingga "gagal konek DB karena pool exhausted" terlihat sama persis
 * dengan "data tidak ditemukan" di mata kode pemanggil (contoh: proses
 * login jadi mengira akun tidak ada / password salah, padahal DB-nya
 * yang lagi overload). Exception ini dipakai supaya pemanggil yang
 * peduli (mis. login) bisa membedakan dua kondisi tersebut.
 *
 * Unchecked (extends RuntimeException) supaya tidak perlu mengubah
 * signature method yang sudah ada di seluruh codebase. Kalau tidak
 * ditangkap secara khusus, exception ini akan naik ke catch(Exception)
 * yang sudah ada di pemanggil (mis. MessageHandler login case) dan
 * koneksi client akan ditutup dengan aman, alih-alih melanjutkan
 * proses dengan asumsi data kosong (yang sebelumnya bisa menyebabkan
 * bug INSERT duplikat pada fashion_setting).
 */
public class DbUnavailableException extends RuntimeException {
    public DbUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

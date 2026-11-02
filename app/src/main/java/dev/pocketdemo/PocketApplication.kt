package dev.pocketdemo

import android.app.Application
import androidx.room.Room
import dev.pocketdemo.data.*
import dev.pocketdemo.security.EncryptedSessionStore

class PocketApplication : Application() {
    val database by lazy { Room.databaseBuilder(this, BankDatabase::class.java, "pocket-demo.db").build() }
    val repository by lazy { LocalBankRepository(database) }
    val session by lazy { EncryptedSessionStore(this).also { it.ensureMockToken() } }
}

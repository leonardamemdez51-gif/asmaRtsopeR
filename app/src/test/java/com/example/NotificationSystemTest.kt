package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.NotificationDao
import com.example.data.local.NotificationEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NotificationSystemTest {

    private lateinit var db: AppDatabase
    private lateinit var notificationDao: NotificationDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        notificationDao = db.notificationDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testInsertAndGetNotifications() = runBlocking {
        val notif = NotificationEntity(
            userId = 1L,
            username = "COBRADOR_1",
            type = "PAGO_RECIBIDO",
            title = "Pago registrado",
            message = "Marta Gómez pagó $100.00 MXN",
            priority = "MEDIA",
            recipientRole = "COBRADOR"
        )
        val id = notificationDao.insertNotification(notif)
        assertTrue(id > 0)

        val list = notificationDao.getNotificationsForUser(1L).first()
        assertEquals(1, list.size)
        assertEquals("Pago registrado", list[0].title)
        assertEquals("MEDIA", list[0].priority)
    }

    @Test
    fun testMarkAsRead() = runBlocking {
        val notif = NotificationEntity(
            userId = 2L,
            username = "COBRADOR_2",
            type = "PAGO_VENCIDO",
            title = "Atraso",
            message = "Cliente atrasado",
            priority = "ALTA"
        )
        val id = notificationDao.insertNotification(notif)
        notificationDao.markAsRead(id)

        val list = notificationDao.getNotificationsForUser(2L).first()
        assertTrue(list[0].isRead)
    }

    @Test
    fun testMarkAllAsRead() = runBlocking {
        notificationDao.insertNotification(NotificationEntity(userId = 3L, type = "ALERTA_CAJA", title = "Caja", message = "Caja abierta"))
        notificationDao.insertNotification(NotificationEntity(userId = 3L, type = "PROMESA_INCUMPLIDA", title = "Promesa", message = "Incumplió"))

        notificationDao.markAllAsReadForUser(3L)
        val list = notificationDao.getNotificationsForUser(3L).first()
        assertTrue(list.all { it.isRead })
    }

    @Test
    fun testDeleteAndClear() = runBlocking {
        val id = notificationDao.insertNotification(NotificationEntity(userId = 4L, type = "NUEVA_AUTORIZACION", title = "Autorizar", message = "Monto grande"))
        notificationDao.deleteNotification(id)
        val list = notificationDao.getNotificationsForUser(4L).first()
        assertTrue(list.isEmpty())
    }

    @Test
    fun testUnreadCount() = runBlocking {
        notificationDao.insertNotification(NotificationEntity(userId = 5L, type = "DOCUMENTO_RECHAZADO", title = "Doc", message = "Rechazado", isRead = false))
        notificationDao.insertNotification(NotificationEntity(userId = 5L, type = "DOCUMENTO_FALTA", title = "Doc", message = "Falta", isRead = true))

        val unreadCount = notificationDao.getUnreadCountForUser(5L).first()
        assertEquals(1, unreadCount)
    }
}

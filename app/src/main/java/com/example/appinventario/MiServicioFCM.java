package com.example.appinventario;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MiServicioFCM extends FirebaseMessagingService {
    @Override
    public void onNewToken(@NonNull String token){
        super.onNewToken(token);
        Log.d("FCM_TOKEN", "El token de este dispositivo es: " + token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage){
        super.onMessageReceived(remoteMessage);

        Log.d("FCM_MESSAGE", "Mensaje recibido desde: " + remoteMessage.getFrom());

        RemoteMessage.Notification mensaje = remoteMessage.getNotification();
        String tituloRecibido = mensaje != null ? mensaje.getTitle() : remoteMessage.getData().get("title");
        String cuerpoRecibido = mensaje != null ? mensaje.getBody() : remoteMessage.getData().get("body");
        String titulo = tituloRecibido == null || tituloRecibido.trim().isEmpty()
                ? "Novedad Scently" : tituloRecibido;
        String cuerpo = cuerpoRecibido == null || cuerpoRecibido.trim().isEmpty()
                ? "Nueva actualización en el inventario." : cuerpoRecibido;

        Log.d("FCM_MESSAGE", "Titulo: " + titulo + " | Contenido: " + cuerpo);
        mostrarNotificacion(titulo, cuerpo);
    }

    private void mostrarNotificacion(String titulo, String cuerpo) {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;

        String canalId = MainActivity.CANAL_ID;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && notificationManager.getNotificationChannel(canalId) == null) {
            NotificationChannel canal = new NotificationChannel(
                    canalId,
                    "Alertas de Inventario",
                    NotificationManager.IMPORTANCE_HIGH
            );
            canal.setDescription("Notificaciones en tiempo real de Scently");
            notificationManager.createNotificationChannel(canal);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w("FCM_MESSAGE", "No se muestra la notificación: falta POST_NOTIFICATIONS");
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, canalId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(titulo)
                .setContentText(cuerpo)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        try {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        } catch (SecurityException error) {
            Log.e("FCM_MESSAGE", "No se pudo publicar la notificación", error);
        }
    }
}

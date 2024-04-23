<?php

require 'vendor/autoload.php';

use Kreait\Firebase\Factory;
use Kreait\Firebase\Messaging\CloudMessage;
use Kreait\Firebase\Messaging\Notification;

// Configuración de Firebase Admin SDK
$factory = (new Factory)->withServiceAccount('../serviceAccount.json');

$messaging = $factory->createMessaging();

// Obtener la lista de tokens de los usuarios
$hostname = "db";
$username = "admin";
$password = "test";
$db = "database";

$con = mysqli_connect($hostname, $username, $password, $db);

if (mysqli_connect_errno()) {
    echo "Failed to connect to MySQL: " . mysqli_connect_error();
    exit();
}

$sql = "SELECT token FROM usuarios";
$result = mysqli_query($con, $sql);

$tokens = [];

while ($row = mysqli_fetch_assoc($result)) {
    $tokens[] = $row['token'];
}

mysqli_close($con);

// Obtener el nombre del animal del cuerpo POST
$pAnimal = $_POST['animal'];

// Crear la notificación
$title = "Nueva foto subida";
$body = "¡Se ha subido una nueva foto de un $pAnimal! ¡Corre y entra a verla!";

$notification = Notification::create($title, $body);

// Enviar la notificación a todos los usuarios
foreach ($tokens as $token) {
    $message = CloudMessage::withTarget('token', $token)
        ->withNotification($notification);

    try {
        $messaging->send($message);
        echo "Notificación enviada a: $token\n";
    } catch (\Kreait\Firebase\Exception\MessagingException $e) {
        echo "Error al enviar la notificación a $token: " . $e->getMessage() . "\n";
    }
}

?>

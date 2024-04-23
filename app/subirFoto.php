<?php
$hostname = "db";
$username = "admin";
$password = "test";
$db = "database";

$con = mysqli_connect($hostname, $username, $password, $db);

// Verificar la conexión
if (mysqli_connect_errno()) {
    echo "Failed to connect to MySQL: " . mysqli_connect_error();
    exit();
}

// Obtener la imagen
$usuario = $_POST['usuario'];
$animal = $_POST['animal'];
$image = $_POST['foto'];
$latitud = $_POST['latitud'];
$longitud = $_POST['longitud'];
$nombreUri = $_POST['nombreUri'];

// Guardar la imagen en el servidor
$uriCompleta = 'http://34.121.128.202:81/uploads/' . $nombreUri;
$uploadDir = 'uploads/'; // Carpeta donde se guardarán las imágenes
$uploadPath = $uploadDir . $nombreUri; // Ruta completa donde se guardará la imagen

// Guardar la imagen en el servidor
if (file_put_contents($uploadPath, base64_decode($image))) {
    // Insertar la URI en la base de datos
    $uri = $_SERVER['HTTP_HOST'] . '/' . $uploadPath; // URI completa de la imagen
    $sql = "INSERT INTO imagenes (usuario, animal, foto, latitud, longitud, nombreUri) VALUES (?, ?, ?, ?, ?, ?)";
    $stmt = mysqli_prepare($con, $sql);
    
    if ($stmt) {
        // Vincular los parámetros a la consulta SQL
        mysqli_stmt_bind_param($stmt, "ssssss", $usuario, $animal, $uri, $latitud, $longitud, $uriCompleta); // sss = tipo de parámetros que se esperan (string, string, string)
        
        // Ejecutar la consulta SQL
        mysqli_stmt_execute($stmt);
        
        if (mysqli_stmt_errno($stmt) != 0) {
            echo 'Error de sentencia: ' . mysqli_stmt_error($stmt);
        } else {
            echo "Imagen almacenada con éxito.";
        }
        
        // Cerrar la sentencia
        mysqli_stmt_close($stmt);
    } else {
        echo "Error al preparar la consulta SQL: " . mysqli_error($con);
    }
} else {
    echo "Error al guardar la imagen.";
}

// Cerrar la conexión
mysqli_close($con);
?>

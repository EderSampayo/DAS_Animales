<?php
$hostname = "db";
$username = "admin";
$password = "test";
$db = "database";

$con = mysqli_connect($hostname, $username, $password, $db);

// Verificar la conexión
if (mysqli_connect_errno()) {
    echo json_encode(["error" => "Failed to connect to MySQL: " . mysqli_connect_error()]);
    exit();
}

// Obtener los parámetros de la solicitud POST
$uriImagen = $_POST['uriImagen']; // Obtenemos la URI de la imagen desde la solicitud POST
$usuario = $_POST['usuario']; // Obtenemos el usuario desde la solicitud POST

$sql = "SELECT COUNT(*) as count FROM imagenes WHERE usuario = ? AND foto = ?";
$stmt = mysqli_prepare($con, $sql);

if ($stmt) {
    // Vincular los parámetros a la consulta SQL
    mysqli_stmt_bind_param($stmt, "ss", $usuario, $uriImagen); // s = tipo de parámetro que se espera (string)

    // Ejecutar la consulta SQL
    mysqli_stmt_execute($stmt);

    // Obtener el resultado de la consulta
    $result = mysqli_stmt_get_result($stmt);
    $row = mysqli_fetch_assoc($result);

    $count = $row['count'];

    if ($count > 0) {
        echo "true";
    } else {
        echo "false";
    }

    // Cerrar el resultado y la sentencia
    mysqli_free_result($result);
    mysqli_stmt_close($stmt);
} else {
    echo json_encode(["error" => "Error al preparar la consulta SQL: " . mysqli_error($con)]);
}

// Cerrar la conexión
mysqli_close($con);
?>

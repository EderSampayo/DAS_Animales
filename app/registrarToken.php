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

// Obtener datos del usuario
$usuario = $_POST['usuario'];
$token = $_POST['token'];

// Insertar nuevo usuario
$sql_insert_user = "INSERT INTO usuarios (usuario, token) VALUES (?, ?)";
$stmt_insert_user = mysqli_prepare($con, $sql_insert_user);

if ($stmt_insert_user) {
    mysqli_stmt_bind_param($stmt_insert_user, "ss", $usuario, $token);
    mysqli_stmt_execute($stmt_insert_user);

    if (mysqli_stmt_errno($stmt_insert_user) != 0) {
        echo 'Error al registrar el usuario: ' . mysqli_stmt_error($stmt_insert_user);
    } else {
        echo "Usuario registrado con éxito.";
    }

    mysqli_stmt_close($stmt_insert_user);
} else {
    echo "Error al preparar la consulta SQL: " . mysqli_error($con);
}

// Cerrar la conexión
mysqli_close($con);
?>

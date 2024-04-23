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

// Consultar las imágenes del usuario
$usuario = $_GET['usuario']; // Obtenemos el usuario desde la URL

$sql = "SELECT animal, foto, latitud, longitud FROM imagenes";
$stmt = mysqli_prepare($con, $sql);

if ($stmt) {
    // Vincular los parámetros a la consulta SQL
    //mysqli_stmt_bind_param($stmt, "s", $usuario); // s = tipo de parámetro que se espera (string)

    // Ejecutar la consulta SQL
    mysqli_stmt_execute($stmt);

    // Obtener el resultado de la consulta
    $result = mysqli_stmt_get_result($stmt);

    $data = [];

    while ($row = mysqli_fetch_assoc($result)) {
        $data[] = [
            "animal" => $row['animal'],
            "foto" => $row['foto'],
            "latitud" => $row['latitud'],
            "longitud" => $row['longitud']
        ];
    }

    // Devolver los datos como JSON
    echo json_encode($data);

    // Cerrar el resultado y la sentencia
    mysqli_free_result($result);
    mysqli_stmt_close($stmt);
} else {
    echo json_encode(["error" => "Error al preparar la consulta SQL: " . mysqli_error($con)]);
}

// Cerrar la conexión
mysqli_close($con);
?>

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

// Consultar la cantidad total de imágenes
$sql = "SELECT COUNT(*) as cantidad FROM imagenes";
$stmt = mysqli_prepare($con, $sql);

if ($stmt) {
    // Ejecutar la consulta SQL
    mysqli_stmt_execute($stmt);

    // Obtener el resultado de la consulta
    $result = mysqli_stmt_get_result($stmt);

    $row = mysqli_fetch_assoc($result);

    $data = [
        "cantidad" => $row['cantidad']
    ];

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

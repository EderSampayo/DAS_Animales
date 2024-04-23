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

// Obtener la URI a eliminar
$uriToDelete = $_POST['nombreUri'];

// Verificar si la URI existe en la base de datos
$sql = "SELECT nombreUri FROM imagenes WHERE nombreUri = ?";
$stmt = mysqli_prepare($con, $sql);

if ($stmt) {
    // Vincular los parámetros a la consulta SQL
    mysqli_stmt_bind_param($stmt, "s", $uriToDelete);

    // Ejecutar la consulta SQL
    mysqli_stmt_execute($stmt);

    // Obtener el resultado
    mysqli_stmt_store_result($stmt);
    $numRows = mysqli_stmt_num_rows($stmt);

    if ($numRows > 0) {
        // Eliminar la URI de la base de datos
        $sqlDelete = "DELETE FROM imagenes WHERE nombreUri = ?";
        $stmtDelete = mysqli_prepare($con, $sqlDelete);

        if ($stmtDelete) {
            // Vincular los parámetros a la consulta SQL
            mysqli_stmt_bind_param($stmtDelete, "s", $uriToDelete);

            // Ejecutar la consulta SQL
            mysqli_stmt_execute($stmtDelete);

            if (mysqli_stmt_errno($stmtDelete) != 0) {
                echo 'Error al eliminar la URI: ' . mysqli_stmt_error($stmtDelete);
            } else {
                echo "URI eliminada con éxito.";
            }

            // Cerrar la sentencia
            mysqli_stmt_close($stmtDelete);
        } else {
            echo "Error al preparar la consulta SQL para eliminar: " . mysqli_error($con);
        }
    } else {
        echo "La URI no existe en la base de datos.";
    }

    // Cerrar la sentencia
    mysqli_stmt_close($stmt);
} else {
    echo "Error al preparar la consulta SQL: " . mysqli_error($con);
}

// Cerrar la conexión
mysqli_close($con);
?>

<?php
declare(strict_types=1);

require __DIR__ . '/../src/MediaProcessor.php';

header('Content-Type: application/json; charset=utf-8');

if ($_SERVER['REQUEST_METHOD'] !== 'POST' || ($_SERVER['PATH_INFO'] ?? '') !== '/v1/media/process') {
    http_response_code(404);
    echo json_encode(['error' => 'not_found']);
    exit;
}

$payload = json_decode(file_get_contents('php://input') ?: '', true);
try {
    $result = MediaProcessor::process($payload ?? []);
    http_response_code(200);
    echo json_encode(['status' => 'ok', 'outputs' => $result]);
} catch (InvalidArgumentException $e) {
    http_response_code(422);
    echo json_encode(['error' => $e->getMessage()]);
}

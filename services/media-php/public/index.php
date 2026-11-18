<?php
declare(strict_types=1);

require __DIR__ . '/../src/MediaProcessor.php';

header('Content-Type: application/json; charset=utf-8');

if ($_SERVER['REQUEST_METHOD'] !== 'POST' || ($_SERVER['PATH_INFO'] ?? '') !== '/v1/media/process') {
    http_response_code(404);
    echo json_encode(['error' => 'not_found']);
    exit;
}

// Only callers holding the shared token may use the worker (set MEDIA_WORKER_TOKEN on the server).
$expected = (string) getenv('MEDIA_WORKER_TOKEN');
$given = (string) ($_SERVER['HTTP_X_MEDIA_TOKEN'] ?? '');
if ($expected === '' || !hash_equals($expected, $given)) {
    http_response_code(401);
    echo json_encode(['error' => 'unauthorized']);
    exit;
}

$payload = json_decode(file_get_contents('php://input') ?: '', true);
try {
    $result = MediaProcessor::process(is_array($payload) ? $payload : []);
    http_response_code(200);
    echo json_encode(['status' => 'ok', 'outputs' => $result]);
} catch (InvalidArgumentException $e) {
    http_response_code(422);
    echo json_encode(['error' => $e->getMessage()]);
} catch (Throwable $e) {
    error_log('media worker: ' . $e->getMessage());
    http_response_code(500);
    echo json_encode(['error' => 'internal_error']);
}

<?php
declare(strict_types=1);

final class MediaProcessor
{
    private const ALLOWED_TYPES = ['image', 'video'];
    private const ALLOWED_TARGETS = ['thumb_256', 'display_1080', 'mp4_720'];

    /** @param array<string, mixed> $input */
    public static function process(array $input): array
    {
        $type = $input['type'] ?? '';
        $source = $input['source_url'] ?? '';
        $targets = $input['target'] ?? [];

        if (!in_array($type, self::ALLOWED_TYPES, true)) {
            throw new InvalidArgumentException('invalid_type');
        }
        if (!is_string($source) || !filter_var($source, FILTER_VALIDATE_URL)) {
            throw new InvalidArgumentException('invalid_source_url');
        }
        if (!is_array($targets) || array_diff($targets, self::ALLOWED_TARGETS)) {
            throw new InvalidArgumentException('invalid_target');
        }

        // Real processing (GD/Imagick for images, FFmpeg for video) is added in phase 4.
        $outputs = [];
        foreach ($targets as $t) {
            $outputs[$t] = 'queued:' . hash('sha256', $source . $t);
        }
        return $outputs;
    }
}

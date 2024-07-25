<?php
namespace App\Imports;

use App\Models\Question;
use Maatwebsite\Excel\Concerns\ToModel;
use Maatwebsite\Excel\Concerns\WithHeadingRow;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Str;

class QuestionsImport implements ToModel, WithHeadingRow
{
    protected $challengeNo;

    public function __construct($challengeNo = null)
    {
        $this->challengeNo = $challengeNo;
    }

    public function model(array $row)
    {
        Log::info('Processing row: ' . json_encode($row));

        if (!isset($row['questionid']) || !isset($row['question'])) {
            Log::error('Missing required fields in row: ' . json_encode($row));
            return null; // Skip this row
        }

        try {
            return new Question([
                'id' => $row['questionid'] ?? Str::uuid()->toString(),
                'questionId' => $row['questionid'],
                'question' => $row['question'],
                'challengeNo' => $this->challengeNo ?? $row['challengeno'] ?? 1, // Default to 1 if not provided
            ]);
        } catch (\Exception $e) {
            Log::error('Error processing row: ' . json_encode($row));
            Log::error('Error message: ' . $e->getMessage());
            return null; // Skip this row
        }
    }
}
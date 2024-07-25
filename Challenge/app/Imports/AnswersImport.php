<?php
namespace App\Imports;

use App\Models\Answer;
use Maatwebsite\Excel\Concerns\ToModel;
use Maatwebsite\Excel\Concerns\WithHeadingRow;

class AnswersImport implements ToModel, WithHeadingRow
{
    public function model(array $row)
    {
        return new Answer([
            'questionId' => $row['questionid'] ?? null,
            'answerId' => $row['answerid'] ?? null,
            'answer' => $row['answer'] ?? '',
            'mark' => $row['mark'] ?? 0,
            'challengeNo' => $row['challengeno'] ?? null, // Add this line
        ]);
        
    }

}
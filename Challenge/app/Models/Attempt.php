<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class Attempt extends Model
{
    use HasFactory;
    public function questions()
{
    return $this->belongsToMany(Question::class, 'attempt_question')
                ->withPivot('is_correct', 'selected_answer', 'time_spent')
                ->withTimestamps();
}
}

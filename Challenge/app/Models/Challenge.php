<?php
namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\HasMany;

class Challenge extends Model
{
    use HasFactory;

    protected $fillable = ['challengeNo', 'challengeName', 'startDate', 'endDate', 'duration', 'numOfQuestions'];

    public function questions()
    {
        return $this->belongsToMany(Question::class);
    }
    public function getRandomQuestions($limit = 10)
    {
        return $this->questions()->inRandomOrder()->limit($limit)->get();
    }
     
}
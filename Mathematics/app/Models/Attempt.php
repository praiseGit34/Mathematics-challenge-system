<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class Attempt extends Model
{
    //use HasFactory;
  

    public function participant()
    {
        return $this->belongsTo(Participant::class);
    }

    public function challenge()
    {
        return $this->belongsTo(Challenge::class);
    }

    public function answers()
    {
        return $this->hasMany(AttemptAnswer::class);
    }
}


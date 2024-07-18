<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class Participant extends Model
{
    //use HasFactory;
    

    public function school()
    {
        return $this->belongsTo(School::class);
    }

    public function attempts()
    {
        return $this->hasMany(Attempt::class);
    }
}


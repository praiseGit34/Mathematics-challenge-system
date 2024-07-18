<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class School extends Model
{
    //use HasFactory;
    

    public function representative()
    {
        return $this->hasOne(Representative::class);
    }

    public function participants()
    {
        return $this->hasMany(Participant::class);
    }
}


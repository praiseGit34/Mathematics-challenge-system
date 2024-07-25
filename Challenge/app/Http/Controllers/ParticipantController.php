<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;

class ParticipantController extends Controller
{
    // ParticipantController.php
public function viewChallenges()
{
    $challenges = Challenge::where('start_date', '<=', now())->where('end_date', '>=', now())->get();
    return view('participant.view_challenges', compact('challenges'));
}

public function attemptChallenge($id)
{
    $challenge = Challenge::find($id);
    $questions = $challenge->questions()->inRandomOrder()->take($challenge->number_of_questions)->get();
    return view('participant.attempt_challenge', compact('challenge', 'questions'));
}

}

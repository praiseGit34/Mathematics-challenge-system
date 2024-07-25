<?php

namespace App\Http\Controllers;

use App\Models\Challenge;
use App\Models\Question;
use Illuminate\Http\Request;

class ChallengeController extends Controller
{
    public function index()
    {
        $challenges = Challenge::all();
        return view('challenges.index', ['challenges' => $challenges]);
    }

    public function create()
    {
        return view('challenges.create', [
            'title' => 'Create Challenge',
            'activePage' => 'create'
        ]);
    }

    public function store(Request $request)
{
    $validatedData = $request->validate([
        'challengeNo' => 'required',
        'challengeName' => 'required',
        'startDate' => 'required|date',
        'endDate' => 'required|date|after:startDate',
        'duration' => 'required|integer',
        'numOfQuestions' => 'required|integer',
    ]);

    $challenge = new Challenge();
    $challenge->challengeNo = $validatedData['challengeNo'];
    $challenge->challengeName = $validatedData['challengeName'];
    $challenge->startDate = $validatedData['startDate'];
    $challenge->endDate = $validatedData['endDate'];
    $challenge->duration = $validatedData['duration'];
    $challenge->numOfQuestions = $validatedData['numOfQuestions'];

    $challenge->save();
    
     // Randomly select questions for this challenge
     $questions = Question::inRandomOrder()->take($request->input('numOfQuestions'))->get();
     $challenge->questions()->sync($questions);
 
     return redirect()->route('challenges.index')
         ->with('success', 'Challenge created successfully.');
 }


    public function show($id)
    {
        $challenge = Challenge::findOrFail($id);
        return view('challenges.show', compact('challenge'));
    }

    public function edit($id)
    {
        $challenge = Challenge::findOrFail($id);
        return view('challenges.edit', compact('challenge'));
    }

    public function update(Request $request, $id)
    {
        $request->validate([
            'challengeNo' => 'required',
            'challengeName' => 'required',
            'startDate' => 'required',
            'endDate' => 'required',
            'duration' => 'required',
            'numOfQuestions' => 'required',
        ]);

        $challenge = Challenge::findOrFail($id);
        $challenge->update($request->all());

        return redirect()->route('challenges.index')
            ->with('success', 'Challenge updated successfully.');
    }

    public function destroy($id)
    {
        $challenge = Challenge::findOrFail($id);
        $challenge->delete();

        return redirect()->route('challenges.index')
            ->with('success', 'Challenge deleted successfully.');
    }
    
    
}
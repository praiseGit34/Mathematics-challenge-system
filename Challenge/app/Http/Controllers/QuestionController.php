<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use App\Models\Question;
use Maatwebsite\Excel\Facades\Excel;
use App\Imports\QuestionsImport;

class QuestionController extends Controller
{
    public function create()
    {
        return view('uploadquestions');
    }

    public function store(Request $request)
    {
        $request->validate([
            'question_file' => 'required|mimes:xls,xlsx|max:2048', // max size in KB
        ]);

        $file = $request->file('question_file');

        try {
            Excel::import(new QuestionsImport, $file);
            return back()->with('success', 'Questions uploaded successfully!');
        } catch (\Exception $e) {
            return back()->with('error', 'An error occurred while importing the questions: ' . $e->getMessage());
        }
    }
}
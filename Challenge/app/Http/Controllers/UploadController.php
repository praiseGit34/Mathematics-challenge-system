<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use Maatwebsite\Excel\Facades\Excel;
use App\Imports\QuestionsImport;
use App\Imports\AnswersImport;

class UploadController extends Controller
{
    public function create()
    {
        return view('upload');
    }

    public function store(Request $request)
    {
        $request->validate([
            'questions_file' => 'required|file|mimes:xlsx,xls',
            'answers_file' => 'required|file|mimes:xlsx,xls',

        ]);
    
        
        try {
            Excel::import(new QuestionsImport, $request->file('questions_file'));
            Excel::import(new AnswersImport, $request->file('answers_file'));

            return back()->with('success', 'Files uploaded and data imported successfully.');
        } catch (\Exception $e) {
            return back()->withErrors(['error' => 'An error occurred while importing the data: ' . $e->getMessage()]);
        }
    }
}
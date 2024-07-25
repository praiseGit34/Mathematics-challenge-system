<?php

namespace App\Http\Controllers;

use App\Models\School;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Hash;

class SchoolController extends Controller
{
    public function index()
    {
        $schools = School::all();
        return view('schools.index', compact('schools'));
    }

    public function create()
    {
        return view('schools.create');
    }

    public function store(Request $request)
    {
        $request->validate([
            'name' => 'required|string|max:255',
            'district' => 'required|string|max:255',
            'schoolRegNo' => 'required|string|max:255',
            'emailAddress' => 'required|email|unique:schools,emailAddress',
            'nameOfRep' => 'required|string|max:255',
            'password' => 'required|string|min:8', // Add password validation
        ]);

        $schoolData = $request->except('password');
        $schoolData['password'] = Hash::make($request->password);

        School::create($schoolData);

        return redirect()->route('schools.index')->with('success', 'School added successfully.');
    }

    public function edit(School $school)
    {
        return view('schools.edit', compact('school'));
    }

    public function update(Request $request, School $school)
    {
        $request->validate([
            'name' => 'required|string|max:255',
            'district' => 'required|string|max:255',
            'schoolRegNo' => 'required|string|max:255',
            'emailAddress' => 'required|email|unique:schools,emailAddress,' . $school->id,
            'nameOfRep' => 'required|string|max:255',
            'password' => 'nullable|string|min:8', // Password is optional on update
        ]);

        $schoolData = $request->except('password');
        
        if ($request->filled('password')) {
            $schoolData['password'] = Hash::make($request->password);
        }

        $school->update($schoolData);

        return redirect()->route('schools.index')->with('success', 'School updated successfully.');
    }

    public function destroy(School $school)
    {
        $school->delete();

        return redirect()->route('schools.index')->with('success', 'School deleted successfully.');
    }
}
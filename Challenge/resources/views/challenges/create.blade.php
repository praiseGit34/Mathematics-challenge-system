@extends('layouts.app')

@section('content')
<div class="container">
    <h1>Create New Challenge</h1>
    <form method="POST" action="{{ route('challenges.store') }}">
        @csrf
        <div class="form-group">
            <label for="challengeNo">Challenge No</label>
            <input type="text" class="form-control" id="challengeNo" name="challengeNo" required>
        </div>
        <div class="form-group">
            <label for="challengeName">Challenge Name</label>
            <input type="text" class="form-control" id="challengeName" name="challengeName" required>
        </div>
        <div class="form-group">
            <label for="startDate">Start Date</label>
            <input type="datetime-local" class="form-control" id="startDate" name="startDate" required>
        </div>
        <div class="form-group">
            <label for="endDate">End Date</label>
            <input type="datetime-local" class="form-control" id="endDate" name="endDate" required>
        </div>
        <div class="form-group">
            <label for="duration">Duration (in minutes)</label>
            <input type="number" class="form-control" id="duration" name="duration" required>
        </div>
        <div class="form-group">
            <label for="numOfQuestions">Number of Questions</label>
            <input type="number" class="form-control" id="numOfQuestions" name="numOfQuestions" required>
        </div>
        <button type="submit" class="btn btn-primary">Create Challenge</button>
    </form>
</div>
@endsection
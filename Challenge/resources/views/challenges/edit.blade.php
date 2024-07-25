@extends('layouts.app')

@section('content')
<div class="container">
    <h1>Edit Challenge</h1>
    <form method="POST" action="{{ route('challenges.update', $challenge->id) }}">
        @csrf
        @method('PUT')
        <div class="form-group">
            <label for="challengeNo">Challenge No</label>
            <input type="text" class="form-control" id="challengeNo" name="challengeNo" value="{{ $challenge->challengeNo }}" required>
        </div>
        <div class="form-group">
            <label for="challengeName">Challenge Name</label>
            <input type="text" class="form-control" id="challengeName" name="challengeName" value="{{ $challenge->challengeName }}" required>
        </div>
        <div class="form-group">
            <label for="startDate">Start Date</label>
            <input type="datetime-local" class="form-control" id="startDate" name="startDate" value="{{ date('Y-m-d\TH:i', strtotime($challenge->startDate)) }}" required>
        </div>
        <div class="form-group">
            <label for="endDate">End Date</label>
            <input type="datetime-local" class="form-control" id="endDate" name="endDate" value="{{ date('Y-m-d\TH:i', strtotime($challenge->endDate)) }}" required>
        </div>
        <div class="form-group">
            <label for="duration">Duration (in minutes)</label>
            <input type="number" class="form-control" id="duration" name="duration" value="{{ $challenge->duration }}" required>
        </div>
        <div class="form-group">
            <label for="numOfQuestions">Number of Questions</label>
            <input type="number" class="form-control" id="numOfQuestions" name="numOfQuestions" value="{{ $challenge->numOfQuestions }}" required>
        </div>
        <button type="submit" class="btn btn-primary">Update Challenge</button>
    </form>
</div>
@endsection
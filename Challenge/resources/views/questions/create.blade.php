@extends('layouts.app')

@section('title', 'Add New Question')

@section('content')
<div class="container">
    <h1>Add New Question</h1>
    <form method="POST" action="{{ route('questions.store') }}">
        @csrf
        <div class="form-group">
            <label for="question">Question</label>
            <textarea class="form-control" id="question" name="question" required></textarea>
        </div>
        <div class="form-group">
            <label for="answer">Answer</label>
            <input type="text" class="form-control" id="answer" name="answer" required>
        </div>
        <div class="form-group">
            <label for="marks">Marks</label>
            <input type="number" class="form-control" id="marks" name="marks" required>
        </div>
        <button type="submit" class="btn btn-primary">Add Question</button>
    </form>
</div>
@endsection
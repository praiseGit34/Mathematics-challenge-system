@extends('layouts.app')

@section('title', 'Questions and Answers')

@section('content')
<div class="container">
    <h1>Questions and Answers</h1>
    <a href="{{ route('questions.create') }}" class="btn btn-primary mb-3">Add New Question</a>
    
    <table class="table">
        <thead>
            <tr>
                <th>Question</th>
                <th>Answer</th>
                <th>Marks</th>
                <th>Actions</th>
            </tr>
        </thead>
        <tbody>
            @foreach($questions as $question)
            <tr>
                <td>{{ $question->question }}</td>
                <td>{{ $question->answer }}</td>
                <td>{{ $question->marks }}</td>
                <td>
                    <a href="{{ route('questions.edit', $question->id) }}" class="btn btn-sm btn-info">Edit</a>
                    <form action="{{ route('questions.destroy', $question->id) }}" method="POST" class="d-inline">
                        @csrf
                        @method('DELETE')
                        <button type="submit" class="btn btn-sm btn-danger" onclick="return confirm('Are you sure?')">Delete</button>
                    </form>
                </td>
            </tr>
            @endforeach
        </tbody>
    </table>
</div>
@endsection
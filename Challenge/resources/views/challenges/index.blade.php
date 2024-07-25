@extends('layouts.app')

@section('content')
<div class="container">
    <h1>All Challenges</h1>
    <a href="{{ route('challenges.create') }}" class="btn btn-primary mb-3">Create New Challenge</a>

    @if (session('success'))
        <div class="alert alert-success">
            {{ session('success') }}
        </div>
    @endif

    <table class="table">
        <thead>
            <tr>
                <th>Challenge No</th>
                <th>Challenge Name</th>
                <th>Start Date</th>
                <th>End Date</th>
                <th>Duration</th>
                <th>Number of Questions</th>
                <th>Actions</th>
            </tr>
        </thead>
        <tbody>
            @foreach ($challenges as $challenge)
                <tr>
                    <td>{{ $challenge->challengeNo }}</td>
                    <td>{{ $challenge->challengeName }}</td>
                    <td>{{ $challenge->startDate }}</td>
                    <td>{{ $challenge->endDate }}</td>
                    <td>{{ $challenge->duration }} minutes</td>
                    <td>{{ $challenge->numOfQuestions }}</td>
                    <td>
                        <a href="{{ route('challenges.show', $challenge->id) }}" class="btn btn-sm btn-info">View</a>
                        <a href="{{ route('challenges.edit', $challenge->id) }}" class="btn btn-sm btn-primary">Edit</a>
                        <form action="{{ route('challenges.destroy', $challenge->id) }}" method="POST" style="display: inline-block;">
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
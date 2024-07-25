@extends('layouts.app')

@section('content')
<div class="container">
    <h1>Challenge Details</h1>
    <div class="card">
        <div class="card-body">
            <h5 class="card-title">{{ $challenge->challengeName }}</h5>
            <p class="card-text"><strong>Challenge No:</strong> {{ $challenge->challengeNo }}</p>
            <p class="card-text"><strong>Start Date:</strong> {{ $challenge->startDate }}</p>
            <p class="card-text"><strong>End Date:</strong> {{ $challenge->endDate }}</p>
            <p class="card-text"><strong>Duration:</strong> {{ $challenge->duration }} minutes</p>
            <p class="card-text"><strong>Number of Questions:</strong> {{ $challenge->numOfQuestions }}</p>
            <a href="{{ route('challenges.edit', $challenge->id) }}" class="btn btn-primary">Edit</a>
            <a href="{{ route('challenges.index') }}" class="btn btn-secondary">Back to List</a>
        </div>
    </div>
</div>
@endsection
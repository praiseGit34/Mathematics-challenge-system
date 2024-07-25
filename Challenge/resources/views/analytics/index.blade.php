@extends('layouts.app')

@section('title', 'Analytics')

@section('content')
<div class="container">
    <h1>Analytics Dashboard</h1>

    <section>
        <h2>Most Correctly Answered Questions</h2>
        <ul>
            @foreach($analytics['mostCorrectQuestions'] as $question)
                <li>{{ $question->content }} (Correct answers: {{ $question->correct_count }})</li>
            @endforeach
        </ul>
    </section>

    <section>
        <h2>School Rankings</h2>
        <ol>
            @foreach($analytics['schoolRankings'] as $school)
                <li>{{ $school->name }} (Participants: {{ $school->participants_count }})</li>
            @endforeach
        </ol>
    </section>

    <section>
        <h2>School Performance Over Time</h2>
        <!-- You might want to use a charting library like Chart.js to visualize this data -->
        @foreach($analytics['schoolPerformanceOverTime'] as $school)
            <h3>{{ $school->name }}</h3>
            <!-- Add your chart or data visualization here -->
        @endforeach
    </section>

    <section>
        <h2>Participant Performance Over Time</h2>
        <!-- Similar to school performance, you might want to use a chart here -->
    </section>

    <section>
        <h2>Question Repetition Percentage</h2>
        <ul>
            @foreach($analytics['questionRepetitionPercentage'] as $data)
                <li>{{ $data['participant'] }}: {{ number_format($data['repetition_percentage'], 2) }}%</li>
            @endforeach
        </ul>
    </section>

    <section>
        <h2>Worst Performing Schools</h2>
        <ol>
            @foreach($analytics['worstPerformingSchools'] as $school)
                <li>{{ $school->name }} (Avg Score: {{ number_format($school->participants_attempts_avg_score, 2) }})</li>
            @endforeach
        </ol>
    </section>

    <section>
        <h2>Best Performing Schools</h2>
        <ol>
            @foreach($analytics['bestPerformingSchools'] as $school)
                <li>{{ $school->name }} (Avg Score: {{ number_format($school->participants_attempts_avg_score, 2) }})</li>
            @endforeach
        </ol>
    </section>

    <section>
        <h2>Participants with Incomplete Attempts</h2>
        <ul>
            @foreach($analytics['participantsWithIncompleteAttempts'] as $participant)
                <li>{{ $participant->name }} (Incomplete attempts: {{ $participant->attempts->where('is_complete', false)->count() }})</li>
            @endforeach
        </ul>
    </section>
</div>
@endsection
<?php

namespace App\Http\Controllers;

use App\Models\Challenge;
use App\Models\School;
use App\Models\Participant;
use App\Models\Question;
use App\Models\Attempt;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;

class AnalyticsController extends Controller
{
    public function index()
    {
        $analytics = [
            'mostCorrectQuestions' => $this->getMostCorrectQuestions(),
            'schoolRankings' => $this->getSchoolRankings(),
            'schoolPerformanceOverTime' => $this->getSchoolPerformanceOverTime(),
            'participantPerformanceOverTime' => $this->getParticipantPerformanceOverTime(),
            'questionRepetitionPercentage' => $this->getQuestionRepetitionPercentage(),
            'worstPerformingSchools' => $this->getWorstPerformingSchools(),
            'bestPerformingSchools' => $this->getBestPerformingSchools(),
            'participantsWithIncompleteAttempts' => $this->getParticipantsWithIncompleteAttempts(),
        ];

        return view('analytics.index', compact('analytics'));
    }

    private function getMostCorrectQuestions()
    {
        return Question::select('questions.*', DB::raw('COUNT(*) as correct_count'))
            ->join('attempt_question', 'questions.id', '=', 'attempt_question.question_id')
            ->where('attempt_question.is_correct', true)
            ->groupBy('questions.id')
            ->orderByDesc('correct_count')
            ->limit(10)
            ->get();
    }

    private function getSchoolRankings()
    {
        return School::withCount(['participants' => function ($query) {
            $query->whereHas('attempts', function ($q) {
                $q->where('is_complete', true);
            });
        }])
        ->orderByDesc('participants_count')
        ->get();
    }

    private function getSchoolPerformanceOverTime()
    {
        // This is a simplified version. You might want to adjust based on your specific needs
        return School::with(['participants.attempts' => function ($query) {
            $query->select('id', 'participant_id', 'challenge_id', 'score', 'created_at')
                ->orderBy('created_at');
        }])->get();
    }

    private function getParticipantPerformanceOverTime()
    {
        return Participant::with(['attempts' => function ($query) {
            $query->select('id', 'participant_id', 'challenge_id', 'score', 'created_at')
                ->orderBy('created_at');
        }])->get();
    }

    private function getQuestionRepetitionPercentage()
    {
        // This is a placeholder. The actual implementation would depend on your data structure
        // and how you define "repetition"
        return Participant::with('attempts.questions')->get()->map(function ($participant) {
            $totalQuestions = $participant->attempts->flatMap->questions->count();
            $uniqueQuestions = $participant->attempts->flatMap->questions->unique()->count();
            return [
                'participant' => $participant->name,
                'repetition_percentage' => ($totalQuestions - $uniqueQuestions) / $totalQuestions * 100
            ];
        });
    }

    private function getWorstPerformingSchools()
    {
        return School::withAvg('participants.attempts', 'score')
            ->orderBy('participants_attempts_avg_score')
            ->limit(10)
            ->get();
    }

    private function getBestPerformingSchools()
    {
        return School::withAvg('participants.attempts', 'score')
            ->orderByDesc('participants_attempts_avg_score')
            ->limit(10)
            ->get();
    }

    private function getParticipantsWithIncompleteAttempts()
    {
        return Participant::whereHas('attempts', function ($query) {
            $query->where('is_complete', false);
        })->with('attempts')->get();
    }
}
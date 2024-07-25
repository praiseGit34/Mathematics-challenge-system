<?php

use Illuminate\Support\Facades\Route;
use Illuminate\Support\Facades\Auth;
use App\Http\Controllers\SchoolController;
use App\Http\Controllers\ChallengeController;
use App\Http\Controllers\QuestionController;
use App\Http\Controllers\AnswerController;
use App\Http\Controllers\UploadController;
use App\Http\Controllers\PageController;
use App\Http\Controllers\HomeController;
use App\Http\Controllers\UserController;
use App\Http\Controllers\ProfileController;
use App\Http\Controllers\AnalyticsController;


Route::get('/', function () {
    return view('welcome');
});

Auth::routes();

Route::get('/home', [HomeController::class, 'index'])->name('home');

// Upload routes (for questions and answers)
Route::get('/upload', [UploadController::class, 'create'])->name('upload.create');
Route::post('/upload', [UploadController::class, 'store'])->name('upload.store');

// Challenge routes
Route::resource('challenges', ChallengeController::class);


Route::get('/analytics', [AnalyticsController::class, 'index'])->name('analytics.index');

// School routes
Route::get('/uploadschools', [SchoolController::class, 'create'])->name('schools.create');
Route::post('/uploadschools', [SchoolController::class, 'store'])->name('schools.store');
Route::get('/schools', [SchoolController::class, 'index'])->name('schools.index');
Route::get('/schools/create', [SchoolController::class, 'create'])->name('schools.create');
Route::post('/schools', [SchoolController::class, 'store'])->name('schools.store');
Route::resource('schools', SchoolController::class);
// Question routes
Route::get('/uploadquestions', [QuestionController::class, 'create'])->name('questions.create');
Route::post('/uploadquestions', [QuestionController::class, 'store'])->name('questions.store');

// Page routes
Route::get('{page}', [PageController::class, 'index'])->name('page.index');

// Protected routes
Route::middleware(['auth'])->group(function () {
    // User management
    Route::resource('user', UserController::class, ['except' => ['show']]);
    
    // Profile routes
    Route::get('profile', [ProfileController::class, 'edit'])->name('profile.edit');
    Route::patch('profile', [ProfileController::class, 'update'])->name('profile.update');
    Route::patch('profile/password', [ProfileController::class, 'password'])->name('profile.password');
});
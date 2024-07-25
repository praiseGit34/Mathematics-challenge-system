<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateAttemptQuestionTable extends Migration
{
    public function up()
    {
        Schema::create('attempt_question', function (Blueprint $table) {
            $table->id();
            $table->unsignedBigInteger('attempt_id');
            $table->unsignedBigInteger('question_id');
            $table->boolean('is_correct')->default(false);
            $table->text('selected_answer')->nullable();
            $table->integer('time_spent')->nullable(); // in seconds
            $table->timestamps();

            $table->foreign('attempt_id')->references('id')->on('attempts')->onDelete('cascade');
            $table->foreign('question_id')->references('id')->on('questions')->onDelete('cascade');

            // Ensure each question is only associated once per attempt
            $table->unique(['attempt_id', 'question_id']);
        });
    }

    public function down()
    {
        Schema::dropIfExists('attempt_question');
    }
}
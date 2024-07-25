<?php
use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateAttemptsTable extends Migration
{
    public function up()
    {
        Schema::create('attempts', function (Blueprint $table) {
            $table->id();
            $table->foreignId('challengeNo')->constrained();
           
            $table->foreignId('participantId')->constrained();
            $table->time('startTime');
            $table->time('endTime');
            $table->integer('score');
            $table->integer('percentageMark');
            $table->timestamp('completed_at')->nullable();
            $table->timestamps();
        });
    }

    public function down()
    {
        Schema::dropIfExists('attempts');
    }
}

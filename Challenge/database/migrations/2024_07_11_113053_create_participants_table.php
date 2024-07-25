<?php
use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateParticipantsTable extends Migration
{
    public function up()
    {
        Schema::create('participants', function (Blueprint $table) {
            $table->id();
            $table->string('username',50)->unique();
            $table->string('firstName');
            $table->string('lastName');
            $table->string('email',191)->unique();
            $table->date('dateOfBirth');
            $table->string('schoolRegNo',50);
            $table->string('imagepath');
            $table->string('password')->nullable();
            $table->timestamps();

            $table->foreign('schoolRegNo')->references('schoolRegNo')->on('schools');
        });
    }

    public function down()
    {
        Schema::dropIfExists('participants');
    }
}

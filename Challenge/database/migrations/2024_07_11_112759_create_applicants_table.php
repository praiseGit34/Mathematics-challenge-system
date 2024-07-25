<?php
use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateApplicantsTable extends Migration
{
    public function up()
    {
        Schema::create('applicants', function (Blueprint $table) {
            $table->id();
            $table->string('userName',50)->unique();
            $table->string('firstName');
            $table->string('lastName');
            $table->string('email',191)->unique();
            $table->date('dateOfBirth');
            $table->string('schoolRegNo',50);
            $table->string('imagePath');
            $table->string('password')->nullable();
            $table->timestamps();

            $table->foreign('schoolRegNo')->references('schoolReNo')->on('schools');
        });
    }

    public function down()
    {
        Schema::dropIfExists('applicants');
    }
}
